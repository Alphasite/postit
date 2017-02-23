node {
   stage('Preparation') { 
      	
   }
   
   stage('Build') {
        if(isUnix()){
            sh './gradlew build --info'
        } else{
            bat 'gradlew build --info'
        }
   }
   
   stage('Results') {
        junit '**/target/surefire-reports/TEST-*.xml'
        archive 'target/*.jar'
   }
}
