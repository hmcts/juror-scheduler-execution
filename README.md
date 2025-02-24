# API Execution Service

### Environmental Variables
```
SECRET=WW91clZlcnlWZXJ5VmVyeVNlY3JldEtleVRoYXRJc1NvU2VjcmV0SURvbnRFdmVuS25vd0l0QnV0Rm9yVGhlRXhlY3V0aW9uTGF5ZXJUaGlzVGltZQ==
SPRING_PROFILES_ACTIVE=test

SCHEDULER_SERVICE_SUBJECT=external-api@juror-scheduler-api.hmcts.net
SCHEDULER_SERVICE_SECRET=WW91clZlcnlWZXJ5VmVyeVNlY3JldEtleVRoYXRJc1NvU2VjcmV0SURvbnRFdmVuS25vd0l0

POLICE_NATIONAL_COMPUTER_CHECK_SERVICE_SUBJECT=api.job.execution.service@schedular.cgi.com
POLICE_NATIONAL_COMPUTER_CHECK_SERVICE_SECRET=WW91clZlcnlWZXJ5VmVyeVNlY3JldEtleVRoYXRJc1NvU2VjcmV0SURvbnRFdmVuS25vd0l0QnV0Rm9yVGhlRXhlY3V0aW9uTGF5ZXJUaGlzVGltZQ==

JUROR_SERVICE_SECRET=W3N1cGVyLXNlY3JldC1rZXktYnVyZWF1XVtzdXBlci1zZWNyZXQta2V5LWJ1cmVhdV1bc3VwZXItc2VjcmV0LWtleS1idXJlYXVd
JUROR_SERVICE_HOST=localhost
JUROR_SERVICE_PORT=8080
```

###
### Building the application 

When running the application for the first time you will need to create an ADMIN user.

To do this you will need to add the following environmental variables which (assuming the user does not already exist) create an admin user for the provided email address and password.

Upon logging in for the first time it is **Highly** recommend you change the password and remove the environment variables.
If you do not remove the credentials from the configuration after first boot the application will force shutdown.
```
ADMIN_EMAIL=admin@scheduler.cgi.com
ADMIN_PASSWORD=kj3TXdvYqmFTXXTq!9nA7ZUmDgiQ&W7Z&v7mnFyp2bvM&BZ#nPosFfL8zNvw
ADMIN_FIRSTNAME=Admin
ADMIN_LASTNAME=Admin
```

###

## Plugins

The template contains the following plugins:

  *

    https://docs.gradle.org/current/userguide/checkstyle_plugin.support

    Performs code style checks on Java source files using Checkstyle and generates reports from these checks.
    The checks are included in gradle's *check* task (you can run them by executing `./gradlew check` command).

  * pmd

    https://docs.gradle.org/current/userguide/pmd_plugin.support

    Performs static code analysis to finds common programming flaws. Included in gradle `check` task.


  * jacoco

    https://docs.gradle.org/current/userguide/jacoco_plugin.support

    Provides code coverage metrics for Java code via integration with JaCoCo.
    You can create the report by running the following command:

    ```bash
      ./gradlew jacocoTestReport
    ```

    The report will be created in build/reports subdirectory in your project directory.

  * io.spring.dependency-management

    https://github.com/spring-gradle-plugins/dependency-management-plugin

    Provides Maven-like dependency management. Allows you to declare dependency management
    using `dependency 'groupId:artifactId:version'`
    or `dependency group:'group', name:'name', version:version'`.

  * org.springframework.boot

    http://projects.spring.io/spring-boot/

    Reduces the amount of work needed to create a Spring application

  * org.owasp.dependencycheck

    https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.support

    Provides monitoring of the project's dependent libraries and creating a report
    of known vulnerable components that are included in the build. To run it
    execute `gradle dependencyCheck` command.

  * com.github.ben-manes.versions

    https://github.com/ben-manes/gradle-versions-plugin

    Provides a task to determine which dependencies have updates. Usage:

    ```bash
      ./gradlew dependencyUpdates -Drevision=release
    ```

## Setup

Located in `./bin/init.sh`. Simply run and follow the explanation how to execute it.

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/spring-boot-template` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `4550` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4550/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
