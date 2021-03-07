pipeline {
  agent {
    kubernetes {
      label 'xtext-xtend-api-diff'
      defaultContainer 'jnlp'
      yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jnlp
    image: 'eclipsecbi/jenkins-jnlp-agent'
    args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
    resources:
      limits:
        memory: "3.5Gi"
        cpu: "1.0"
      requests:
        memory: "3.5Gi"
        cpu: "1.0"
    volumeMounts:
    - name: gradle
      mountPath: /home/jenkins/.gradle
  volumes:
  - name: volume-known-hosts
    configMap:
      name: known-hosts
  - name: gradle
    emptyDir: {}
    '''
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
              ./gradlew clean build
              cp -f build/libs/japicmp-ext.jar ..
            '''
          }
        }
      }
    }
    stage('Create API Diff') {
      steps {
        sh './create-api-diff.sh'
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

          /*
          slackSend (
            message: "${lastResult} => ${curResult}: <${env.BUILD_URL}|${env.JOB_NAME}#${env.BUILD_NUMBER}>",
            botUser: true,
            channel: 'xtext-builds',
            color: "${color}"
          )
          */
        }
      }
    }
  }

}