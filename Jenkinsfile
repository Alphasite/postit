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
   }
}
