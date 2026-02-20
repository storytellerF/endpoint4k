#!/bin/bash

# Script to delete all GitHub packages of a specified version
# Usage: ./delete_github_packages.sh <version> <org_or_user> <token> [package_type] [scope]
# If package_type is not specified, defaults to 'maven'
# If scope is not specified, defaults to 'org' (use 'user' for personal accounts)
# Requires jq for JSON parsing

set -e

if [ $# -lt 3 ]; then
    echo "Usage: $0 <version> <org_or_user> <token> [package_type] [scope]"
    echo "Example: $0 1.0.0 myorg ghp_1234567890abcdef maven org"
    echo "For personal accounts, use scope 'user': $0 1.0.0 myuser ghp_... maven user"
    exit 1
fi

VERSION=$1
ORG_OR_USER=$2
TOKEN=$3
PACKAGE_TYPE=${4:-maven}
SCOPE=${5:-org}

echo "Deleting all $PACKAGE_TYPE packages with version $VERSION from $SCOPE $ORG_OR_USER"

# Get all packages of the specified type
if [ "$SCOPE" = "org" ]; then
    PACKAGES_URL="https://api.github.com/orgs/$ORG_OR_USER/packages?package_type=$PACKAGE_TYPE"
else
    PACKAGES_URL="https://api.github.com/user/packages?package_type=$PACKAGE_TYPE"
fi

RESPONSE=$(curl -s -w "\n%{http_code}" -H "Authorization: token $TOKEN" "$PACKAGES_URL")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
JSON_DATA=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" -ne 200 ]; then
    echo "Error: Failed to fetch packages (HTTP $HTTP_CODE)"
    echo "Response: $JSON_DATA"
    exit 1
fi

PACKAGES=$(echo "$JSON_DATA" | jq -r '.[].name' 2>/dev/null)
if [ $? -ne 0 ]; then
    echo "Error: Invalid JSON response from GitHub API"
    echo "Response: $JSON_DATA"
    exit 1
fi

for PACKAGE in $PACKAGES; do
    echo "Processing package: $PACKAGE"
    
    # Get version IDs for the specified version
    if [ "$SCOPE" = "org" ]; then
        VERSIONS_URL="https://api.github.com/orgs/$ORG_OR_USER/packages/$PACKAGE_TYPE/$PACKAGE/versions"
    else
        VERSIONS_URL="https://api.github.com/user/packages/$PACKAGE_TYPE/$PACKAGE/versions"
    fi
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -H "Authorization: token $TOKEN" "$VERSIONS_URL")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    JSON_DATA=$(echo "$RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" -ne 200 ]; then
        echo "  Error: Failed to fetch versions for package $PACKAGE (HTTP $HTTP_CODE)"
        continue
    fi
    
    VERSION_IDS=$(echo "$JSON_DATA" | jq -r ".[] | select(.name == \"$VERSION\") | .id" 2>/dev/null)
    if [ $? -ne 0 ]; then
        echo "  Error: Invalid JSON response for versions of package $PACKAGE"
        continue
    fi
    
    if [ -z "$VERSION_IDS" ]; then
        echo "  No version $VERSION found for package $PACKAGE"
        continue
    fi
    
    for VID in $VERSION_IDS; do
        echo "  Deleting version $VERSION (ID: $VID) of package $PACKAGE"
        if [ "$SCOPE" = "org" ]; then
            DELETE_URL="https://api.github.com/orgs/$ORG_OR_USER/packages/$PACKAGE_TYPE/$PACKAGE/versions/$VID"
        else
            DELETE_URL="https://api.github.com/user/packages/$PACKAGE_TYPE/$PACKAGE/versions/$VID"
        fi
        RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE -H "Authorization: token $TOKEN" "$DELETE_URL")
        HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
        JSON_DATA=$(echo "$RESPONSE" | head -n -1)
        
        if [ "$HTTP_CODE" -eq 204 ]; then
            echo "    Successfully deleted"
        elif [ "$HTTP_CODE" -eq 400 ] && echo "$JSON_DATA" | grep -q "last version"; then
            echo "    Cannot delete last version, deleting entire package instead"
            if [ "$SCOPE" = "org" ]; then
                DELETE_PACKAGE_URL="https://api.github.com/orgs/$ORG_OR_USER/packages/$PACKAGE_TYPE/$PACKAGE"
            else
                DELETE_PACKAGE_URL="https://api.github.com/user/packages/$PACKAGE_TYPE/$PACKAGE"
            fi
            PACKAGE_RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE -H "Authorization: token $TOKEN" "$DELETE_PACKAGE_URL")
            PACKAGE_HTTP_CODE=$(echo "$PACKAGE_RESPONSE" | tail -n1)
            PACKAGE_JSON_DATA=$(echo "$PACKAGE_RESPONSE" | head -n -1)
            if [ "$PACKAGE_HTTP_CODE" -eq 204 ]; then
                echo "    Successfully deleted package"
            else
                echo "    Failed to delete package (HTTP $PACKAGE_HTTP_CODE)"
                if [ -n "$PACKAGE_JSON_DATA" ]; then
                    echo "    Response: $PACKAGE_JSON_DATA"
                fi
                echo "    Note: Ensure your token has 'delete:packages' permission and appropriate scopes"
            fi
        else
            echo "    Failed to delete (HTTP $HTTP_CODE)"
            if [ -n "$JSON_DATA" ]; then
                echo "    Response: $JSON_DATA"
            fi
        fi
    done
done

echo "Deletion process completed"