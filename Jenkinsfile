node ('beaware-jenkins-slave') {
    stage('Download Latest') {
	if (env.BRANCH_NAME != 'master') {
		autoCancelled = true
		return
	}
        git(url: 'https://github.com/beaware-project/text-analysis-all.git', branch: 'master')
        sh 'git submodule init'
        sh 'git submodule update'
    }

    stage ('Compile (Maven)') {
        sh 'mvn clean package -U'
    }

    stage ('Build docker image') {
		sh 'docker build -t beaware/text-analysis-all:${BUILD_NUMBER} .'
    }

    stage ('Push docker image') {
        withDockerRegistry([credentialsId: 'dockerhub-credentials']) {
            sh 'docker push beaware/text-analysis-all:${BUILD_NUMBER}'
        }
    }

    stage ('Deploy') {
 		sh ''' sed -i 's/IMAGE_TAG/'"$BUILD_NUMBER"'/g' kubernetes/deploy.yaml '''
        sh 'kubectl apply -f kubernetes/deploy.yaml -n prod --validate=false'
    }
    
    stage ('Print-deploy logs') {
        sh 'sleep 60'
        sh 'kubectl  -n prod logs deploy/text-analysis-all -c text-analysis-all'
    }    
}
