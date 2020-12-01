pipeline {
  agent {
    kubernetes {
      label 'xtext-xtend-api-diff-' + env.BUILD_NUMBER
      defaultContainer 'xtext-buildenv'
      yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jnlp
    image: 'eclipsecbi/jenkins-jnlp-agent'
    args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
    volumeMounts:
    - mountPath: /home/jenkins/.ssh
      name: volume-known-hosts
    resources:
      limits:
        memory: "0.5Gi"
        cpu: "0.2"
      requests:
        memory: "0.5Gi"
        cpu: "0.2"
  - name: xtext-buildenv
    image: docker.io/smoht/xtext-buildenv:0.7
    tty: true
    resources:
      limits:
        memory: "3.5Gi"
        cpu: "1.0"
      requests:
        memory: "3.5Gi"
        cpu: "1.0"
    volumeMounts:
    - name: settings-xml
      mountPath: /home/jenkins/.m2/settings.xml
      subPath: settings.xml
      readOnly: true
    - name: toolchains-xml
      mountPath: /home/jenkins/.m2/toolchains.xml
      subPath: toolchains.xml
      readOnly: true
    - name: settings-security-xml
      mountPath: /home/jenkins/.m2/settings-security.xml
      subPath: settings-security.xml
      readOnly: true
    - name: m2-repo
      mountPath: /home/jenkins/.m2/repository
  volumes:
  - name: volume-known-hosts
    configMap:
      name: known-hosts
  - name: settings-xml
    secret:
      secretName: m2-secret-dir
      items:
      - key: settings.xml
        path: settings.xml
  - name: toolchains-xml
    configMap:
      name: m2-dir
      items:
      - key: toolchains.xml
        path: toolchains.xml
  - name: settings-security-xml
    secret:
      secretName: m2-secret-dir
      items:
      - key: settings-security.xml
        path: settings-security.xml
  - name: m2-repo
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
    choice(name: 'OLD_VERSION', choices: ['2.24.0', '2.23.0', '2.22.0', '2.21.0', '2.20.0', '2.19.0', '2.18.0', '2.17.1', '2.17.0'], description: 'Old Version')
    choice(name: 'NEW_VERSION', choices: ['2.25.0', '2.24.0', '2.23.0', '2.22.0', '2.21.0', '2.20.0', '2.19.0', '2.18.0', '2.17.1', '2.17.0'], description: 'New Version')
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