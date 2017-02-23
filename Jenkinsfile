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
   
   stage('Results') {
        junit '**/target/surefire-reports/TEST-*.xml'
        archive 'target/*.jar'
   }
}
