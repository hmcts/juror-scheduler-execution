 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.0
FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/api-job-execution-service.jar /opt/app/

EXPOSE 8080
CMD [ "api-job-execution-service.jar" ]
