node {
   stage('Preparation') { 
      	
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
