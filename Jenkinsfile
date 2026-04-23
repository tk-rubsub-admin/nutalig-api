@Library('shared-jenkins-library@master') _

pipeline {
    agent { label "master" }
    tools { maven 'maven3' }

    parameters {
        string(name: 'APP_NAME', defaultValue: 'api', description: 'You Application Name')
        string(name: 'REPO_NAME', defaultValue: '239247536672.dkr.ecr.ap-southeast-1.amazonaws.com', description: 'You Repo Name')
        string(name: 'KUBE_NAMESPACE', defaultValue: 'application', description: 'Target Kubernetes Namespace')
    }

    stages {
        stage ('Get Latest Version') {
            steps {
                script {
                    latestTag = sh(returnStdout:  true, script: "git tag --sort=-creatordate | head -n 1").trim()
                    env.RELEASE_VERSION = latestTag
                }
            }
        }
        stage('SonarQube Analysis') {
            when {
                expression {
                    return params.ENVIRONMENT == 'dev';
                }
            }
            steps {
                 withSonarQubeEnv('sonarqube') {
                    sonarqubeAnalysis(params.APP_NAME, params.ENVIRONMENT)
                }
            }
        }
        stage("Quality Gate") {
            when {
                expression {
                    return params.ENVIRONMENT == 'dev';
                }
            }
            steps {
                timeout(time: 300, unit: 'SECONDS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage ('Build Jar') {
            steps {
                sh 'mvn -Dmaven.test.failure.ignore=true clean package'
            }
        }
        stage ('Build And Push Image') {
            steps {
                script {
                    buildAndPushImage(
                        params.REPO_NAME,
                        params.APP_NAME,
                        env.RELEASE_VERSION,
                        params.ENVIRONMENT
                    )
                }
            }
        }
        stage ('Helm Deploy') {
            steps {
                script {
                    helmDeploy(
                        params.REPO_NAME,
                        params.APP_NAME,
                        env.RELEASE_VERSION,
                        params.KUBE_NAMESPACE,
                        params.ENVIRONMENT,
                        params.ENABLE_INGRESS,
                        params.PREFIX_URL
                    )
                }
            }
        }
    }

    post {
        always {
            script {
                sendNotifications(currentBuild.result)
            }
        }
    }
}
