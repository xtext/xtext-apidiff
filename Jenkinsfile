pipeline {
  agent {
    kubernetes {
      inheritFrom 'ubuntu-2024'
    }
  }

  options {
    buildDiscarder(logRotator(numToKeepStr:'15'))
    disableConcurrentBuilds()
    timeout(time: 45, unit: 'MINUTES')
  }

  tools {
     jdk "temurin-jdk17-latest"
  }

  environment {
      GRADLE_USER_HOME = "$WORKSPACE/.gradle" // workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=564559
      GITHUB_API_CREDENTIALS_ID = 'github-bot-token'
  }

  // https://jenkins.io/doc/book/pipeline/syntax/#triggers
  triggers {
    cron('35 23 * * *') // nightly at 23:35
  }
  
  parameters {
    choice(name: 'OLD_VERSION', choices: ['2.37.0', '2.36.0', '2.35.0', '2.34.0', '2.33.0', '2.32.0', '2.31.0', '2.30.0', '2.29.0', '2.28.0', '2.27.0', '2.26.0', '2.25.0', '2.24.0'], description: 'Old Version')
    choice(name: 'NEW_VERSION', choices: ['2.38.0', '2.37.0', '2.36.0', '2.35.0', '2.34.0', '2.33.0', '2.32.0', '2.31.0', '2.30.0', '2.29.0', '2.28.0', '2.27.0', '2.26.0', '2.25.0', '2.24.0'], description: 'New Version')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          currentBuild.displayName = "#${BUILD_NUMBER}(${params.OLD_VERSION} -> ${params.NEW_VERSION})"
        }
      }
    }
    stage('Build xtext-apidiff') {
      steps {
        script {
          dir ('xtext-apidiff') {
            sh '''
              ./gradlew clean build --warning-mode all
              cp -f build/libs/japicmp-ext.jar ..
            '''
          }
        }
      }
    }
    stage('Create API Diff') {
      steps {
        withCredentials([string(credentialsId: "${GITHUB_API_CREDENTIALS_ID}", variable: 'GITHUB_API_TOKEN')]) {
          script {
            env.DEV_VERSION = getCurrentXtextVersion("main")
            sh './create-api-diff.sh'
          }
        }
      }
    } // END stage
  } // END stages
  
  post {
    always {
      archiveArtifacts artifacts: 'output/**, eclipse/.director-ws/.metadata/.log'
    }
    cleanup {
      script {
        def curResult = currentBuild.currentResult
        def lastResult = 'NEW'
        if (currentBuild.previousBuild != null) {
          lastResult = currentBuild.previousBuild.result
        }

        if (curResult != 'SUCCESS' || lastResult != 'SUCCESS') {
          def color = ''
          switch (curResult) {
            case 'SUCCESS':
              color = '#00FF00'
              break
            case 'UNSTABLE':
              color = '#FFFF00'
              break
            case 'FAILURE':
              color = '#FF0000'
              break
            default: // e.g. ABORTED
              color = '#666666'
          }

          matrixSendMessage https: true,
            hostname: 'matrix.eclipse.org',
            accessTokenCredentialsId: "matrix-token",
            roomId: '!zbliqcdqsggOFDCUoF:matrix.eclipse.org',
            body: "${lastResult} => ${curResult} ${env.BUILD_URL} | ${env.JOB_NAME}#${env.BUILD_NUMBER}",
            formattedBody: "<div><font color='${color}'>${lastResult} => ${curResult}</font> | <a href='${env.BUILD_URL}' target='_blank'>${env.JOB_NAME}#${env.BUILD_NUMBER}</a></div>"
        }
      }
    }
  }

}

def getCurrentXtextVersion (branch) {
  env.BRANCH_REF="${branch}"
  version = sh (returnStdout: true, 
    script: 'curl -sSL -H "Cookie: logged_in=no" -H "Authorization: token $GITHUB_API_TOKEN" -H "Content-Type: text/plain; charset=utf-8" https://api.github.com/repos/eclipse/xtext/contents/pom.xml?ref=$BRANCH_REF| jq -r ".content" | base64 -d | grep -Po "([0-9]+\\.[0-9]+\\.[0-9]+)-SNAPSHOT"')  
  version = version.replace('-SNAPSHOT','').trim()
  return version
}