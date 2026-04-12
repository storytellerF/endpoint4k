FROM storytellerf/android-in-docker:debian-trixie-mate-openjdk21-dev-latest

ARG USER_NAME

USER $USER_NAME
WORKDIR /home/$USER_NAME

COPY --chown=$USER_NAME:$USER_NAME .devcontainer/custom-entrypoint.sh ./bin/custom-entrypoint.sh
RUN chmod +x ./bin/custom-entrypoint.sh

ENTRYPOINT ["sh", "-c", "$HOME/bin/custom-entrypoint.sh"]
