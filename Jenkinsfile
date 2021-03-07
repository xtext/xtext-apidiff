pipeline {
  agent {
    kubernetes {
      label 'centos-7'
    }
  }

  options {
    buildDiscarder(logRotator(numToKeepStr:'15'))
    disableConcurrentBuilds()
    timeout(time: 45, unit: 'MINUTES')
  }

  // https://jenkins.io/doc/book/pipeline/syntax/#triggers
  triggers {
    cron('35 23 * * *') // nightly at 23:35
  }
  
  parameters {
    choice(name: 'OLD_VERSION', choices: ['2.25.0', '2.24.0', '2.23.0', '2.22.0', '2.21.0', '2.20.0', '2.19.0', '2.18.0', '2.17.1', '2.17.0'], description: 'Old Version')
    choice(name: 'NEW_VERSION', choices: ['2.26.0', '2.25.0', '2.24.0', '2.23.0', '2.22.0', '2.21.0', '2.20.0', '2.19.0', '2.18.0', '2.17.1', '2.17.0'], description: 'New Version')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }
    stage('Create API Diff') {
      steps {
        script {
          currentBuild.displayName = "#${BUILD_NUMBER}(${params.OLD_VERSION} -> ${params.NEW_VERSION})"
        }
        sh "./create-api-diff.sh"
      }
    }
  } // END stages
  
  post {
    always {
      archiveArtifacts artifacts: 'output/**'
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

          slackSend (
            message: "${lastResult} => ${curResult}: <${env.BUILD_URL}|${env.JOB_NAME}#${env.BUILD_NUMBER}>",
            botUser: true,
            channel: 'xtext-builds',
            color: "${color}"
          )
        }
      }
    }
  }

}