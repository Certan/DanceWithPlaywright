/* In your project this flow is:
        1. Jenkins starts pipeline
        2. Pipeline runs Maven command
        3. Maven executes tests
        4. JUnit Platform loads CucumberUiRunner
        5. Cucumber filters scenarios by tag
        6. Playwright runs browser automation
        7. Cucumber writes reports to target/cucumber-reports
        8. Jenkins publishes those reports

       Pipeline is the start of Jenkins file - everything inside it tells Jenkins what to do when it runs this pipeline.
 */

pipeline {

    agent any

    tools {
        maven 'maven-3.9.15'
    }

    options {
        skipDefaultCheckout(true) // We will do checkout manually in the first stage
        timestamps() // adds timestamps to console output
        ansiColor('xterm') // enables colored output in console
        disableConcurrentBuilds() // prevents multiple builds from running at the same time
        buildDiscarder(logRotator(
                numToKeepStr: '30',  // keeps the last 30 builds, discards older ones
                artifactNumToKeepStr: '30'  // keeps artifacts from the last 30 builds
        ))
    }

    triggers {
        // Nightly run at a randomized minute during the 2 AM hour
        cron('H 2 * * *')

        // Useful when Jenkins is local and GitHub webhook is not yet practical
        pollSCM('H/1 * * * *')
    }

    parameters {
        string(
                name: 'CUCUMBER_TAGS',
                defaultValue: '@ui',
                description: 'Cucumber tag expression, e.g. @ui, @ui and @login, @smoke and not @wip'
        )

        choice(
                name: 'BROWSER',
                choices: ['CHROMIUM', 'FIREFOX', 'EDGE'],
                description: 'Browser type to run in Paywright'
        )

        booleanParam(
                name: 'HEADLESS',
                defaultValue: true,
                description: 'Run browser in headless mode'
        )

        booleanParam(
                name: 'TRACE_ENABLED',
                defaultValue: false,
                description: 'Enable Playwright tracing'
        )

        booleanParam(
                name: 'INSTALL_PLAYWRIGHT_BROWSERS',
                defaultValue: true,
                description: 'Install Playwright browsers before test execution'
        )

        booleanParam(
                name: 'RUN_DEPENDENCY_ANALYSIS',
                defaultValue: true,
                description: 'Run Maven dependency usage analysis'
        )

        string(
                name: 'EMAIL_RECIPIENTS',
                defaultValue: 'your.email@example.com',
                description: 'Comma-separated email recipients for build notifications'
        )

        string(
                name: 'TIMEOUT_MINUTES',
                defaultValue: '60',
                description: 'Build timeout in minutes, until which the build will be automatically aborted if not completed'
        )
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m' // Set JVM options for Maven, e.g. to increase memory allocation
        ALLURE_RESULT_DIR = 'target/allure-results' // Directory for Allure results
        CUCUMBER_REPORT_DIR = 'target/cucumber-reports' // Directory for Cucumber reports
        SCREENSHOTS_DIR = 'target/screenshots' // Directory for Playwright screenshots
        TRACES_DIR = 'target/traces' // Directory for Playwright traces
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs() // Clean workspace before checkout
                checkout scm // Checkout source code from version control
            }
        }

        stage('Verify Environment') {
            steps {
                sh 'java -version' // Verify Java is available
                sh 'mvn -version' // Verify Maven is available
                sh 'ls -la' // List files in workspace for debugging
            }
        }

        stage('Install Playwright Browsers') {
            when {
                expression { return params.INSTALL_PLAYWRIGHT_BROWSERS }
                // Only run this stage if the parameter is set to true
            }
            steps {
                sh 'mvn -B exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"'
                // Install  Playwrightbrowsers
            }
        }

        stage('Compile Test Sources') {
            steps {
                sh 'mvn -B -DskipTests clean test-compile' // Compile test sources without running tests
            }
        }

        stage('Dependency Analysis') {
            when {
                expression { return params.RUN_DEPENDENCY_ANALYSIS }
                // Only run this stage if the parameter is set to true
            }
            steps {
                sh 'mvn -B dependency:analyze > target/dependency-analysis.txt || true'
                // Run Maven dependency analysis and save output to a file, ignore non-zero exit code
                archiveArtifacts artifacts: 'target/dependency-analysis.txt', fingerprint: true
                // Archive the dependency analysis report as a build artifact
            }
        }

        stage('Run UI Tests') {
            steps {
                script {
                    timeout(time: params.TIMEOUT_MINUTES.toInteger(), unit: 'MINUTES') {
                        withCredentials([
                                string(credentialsId: 'jasypt-master-password', variable: 'JASYPT_MASTER_PASSWORD')
                        ]) {
                            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                                sh """
                            mvn -B clean test \
                              -Dcucumber.filter.tags="${params.CUCUMBER_TAGS}" \
                              -Dbrowser.type="${params.BROWSER}" \
                              -Dbrowser.headless="${params.HEADLESS}" \
                              -Dtrace.enabled="${params.TRACE_ENABLED}" \
                              -Djasypt.encryptor.password="\$JASYPT_MASTER_PASSWORD"
                        """
                            }
                        }
                    }
                }
            }
        }

        stage('Generate Allure Report') {
            steps {
                script {
                    // 1. Generate Allure HTML report using Maven
                    sh 'mvn -B allure:report || true'

                    // 2. Publish Allure report using Jenkins Allure Plugin
                    if (fileExists('target/allure-results')) {
                        allure(
                                commandline: 'Allure-2',
                                includeProperties: false,
                                jdk: '',
                                results: [[path: 'target/allure-results']]
                        )
                    } else {
                        echo 'No Allure results found. Skipping Jenkins Allure report.'
                    }
                }
            }
        }

        stage('Publish Reports') {
            steps {
                script {
                    if (fileExists('target/cucumber-reports/report.xml')) {
                        junit testResults: 'target/cucumber-reports/report.xml', allowEmptyResults: true
                    } else {
                        echo 'Cucumber XML report was not found. Skipping JUnit publishing.'
                    }
                }

                publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'target/cucumber-reports',
                        reportFiles          : 'report.html',
                        reportName           : 'Cucumber HTML Report'
                ])

                publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'target/site/allure-maven-plugin',
                        reportFiles          : 'index.html',
                        reportName           : 'Allure HTML Report'
                ])

                archiveArtifacts artifacts: '''
            target/cucumber-reports/**,
            target/allure-results/**,
            target/site/allure-maven-plugin/**,
            target/screenshots/**,
            target/traces/**,
            target/dependency-analysis.txt
        '''.stripIndent().trim(), allowEmptyArchive: true, fingerprint: true
            }
        }
    }

    post {
        always {
            script {
                echo "Build finished with status: ${currentBuild.currentResult}"
                echo "Tag expression used: ${params.CUCUMBER_TAGS}"
                echo "Browser: ${params.BROWSER}"
            }
        }
    }

// skipping notifications for now
/*        unsuccessful {
            emailext(
                    subject: "[Jenkins] Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """                        <p>Build failed.</p>
                        <p><b>Job:</b> ${env.JOB_NAME}</p>
                        <p><b>Build:</b> #${env.BUILD_NUMBER}</p>
                        <p><b>Result:</b> ${currentBuild.currentResult}</p>
                        <p><b>Tags:</b> ${params.CUCUMBER_TAGS}</p>
                        <p><b>Browser:</b> ${params.BROWSER}</p>
                        <p><a href="${env.BUILD_URL}">Open Jenkins Build</a></p>
                    """,
                    mimeType: 'text/html',
                    to: "${params.EMAIL_RECIPIENTS}"
            )
        }
*/

}