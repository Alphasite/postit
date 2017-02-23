node {
   stage('Preparation') { 
      	
   }
   
   stage('Build') {
        if(isUnix()){
            sh 'gradle build --info'
        } else{
            bat 'gradle build --info'
        }
   }
   
   stage('Results') {
        junit '**/target/surefire-reports/TEST-*.xml'
        archive 'target/*.jar'
   }
}
