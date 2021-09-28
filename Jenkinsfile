    
    @Library('LSPR') _

    

pipeline{
    agent any
   // agent {label 'perftest'}    
	stages{
        stage('build && SonarQube analysis') {
            steps {
                withSonarQubeEnv(installationName: 'SonarQube') {
                 sh "/var/jenkins_home/sonar/sonar-scanner-4.5.0.2216-linux/bin/sonar-scanner"
                }
            }
        }
               
    }
}


