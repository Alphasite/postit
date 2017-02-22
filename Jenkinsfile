node {
   stage('Preparation') { // for display purposes
      // Get some code from a GitHub repository
      git 'https://github.com/jjl284/postit.git'
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
