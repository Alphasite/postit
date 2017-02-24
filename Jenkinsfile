void setBuildStatus(String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "https://github.com/my-org/my-repo"],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}

node {
   stage('Preparation') { 
       checkout scm      	
   }
   
   stage('Build') {
       sh "./gradlew clean build"
   }
   
   stage('Test') {
       sh "./gradlew test"
   }

   stage('Results') {
        junit '**/test-results/test/TEST-*.xml'
        archive 'target/*.jar'
        setBuildStatus("Build complete", "SUCCESS");
   }
}
