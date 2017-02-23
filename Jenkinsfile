node {
   stage('Preparation') { 
       // Mark the code checkout 'stage'....
       stage 'Checkout'

       // Checkout code from repository
       checkout scm      	
   }
   
   stage('Build') {
       sh "ls"
       sh "echo $PWD"
       sh "./gradlew clean build"
   }
   
   stage('Test') {
       sh "./gradlew test"
   }

   stage('Results') {
        junit '**X/test-results/test/TEST-*.xml'
        archive 'target/*.jar'
   }
}
