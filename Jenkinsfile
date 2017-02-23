node {
   stage('Preparation') { 
      	
   }
   
   stage('Build') {
       sh "./gradlew clean build"
   }
   
   stage('Results') {
        junit '**/target/surefire-reports/TEST-*.xml'
        archive 'target/*.jar'
   }
}
