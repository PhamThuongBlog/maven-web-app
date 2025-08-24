node('jenkins_master_node'){
    // Dùng Maven được cài sẵn trong Jenkins (M3)
    def mvnHome = tool name: 'M3', type: 'maven'
    stage ('1. Clone '){
        git credentialsId: 'xacthuc_github', url: 'https://github.com/PhamThuongBlog/maven-web-app.git'
    }
    stage('2. Build với Maven') {
        timeout(time: 5, unit: 'MINUTES'){
            sh "${mvnHome}/bin/mvn clean package"
            // 1. Stash artifact .war để chuyển sang Docker agent
            stash includes: 'target/maven-web-app.war', name: 'war-file'
            // 2. Nếu Dockerfile không có trong repo, stash luôn
            stash includes: 'Dockerfile', name: 'dockerfile'
        }
    }
    stage('3.1 SonarQube Analysis') {
        timeout(time: 5, unit: 'MINUTES') {
            withSonarQubeEnv('sonarqube') {
                sh """
                    ${mvnHome}/bin/mvn sonar:sonar \
                    -Dsonar.projectKey=maven-web-app \
                    -Dsonar.host.url=http://host.docker.internal:9000 \
                    -Dsonar.login=sqa_73fc64bf979dce060bcef479bb212a833c0c0a87
                """
            }
        }
    }
    stage('3.2 Test') {
        sh 'mvn test'
    }
    stage('4. Release artifact to Nexus Repos') {
        timeout(time: 5, unit: 'MINUTES') {
            nexusArtifactUploader(
                artifacts: [[
                    artifactId: 'maven-web-app',
                    classifier: '',
                    file: 'target/maven-web-app.war',
                    type: 'war'
                ]],
                credentialsId: 'Nexus-Credentials',
                groupId: 'in.ashokit',
                nexusUrl: 'host.docker.internal:8081',
                nexusVersion: 'nexus3',
                protocol: 'http',
                repository: 'ashokit.thuong-snapshot-repository',
                version: '3.0-SNAPSHOT'
            )
        }
    }
}
node('docker'){
    stage('5.1 Prepare Docker Build') {
        timeout(time: 5, unit: 'MINUTES') {
            // Lấy artifact và Dockerfile từ master
            unstash 'war-file'
            unstash 'dockerfile'
        }
    }
    stage('5.2 Build Docker Image') {
        timeout(time: 5, unit: 'MINUTES') {
            bat 'docker build -t myapp:latest .'
            //bat 'docker-compose build'
        }
    }
    stage('6. Run App') {
        timeout(time: 5, unit: 'MINUTES') {
            //1. Nếu container đã tồn tại, dừng và xóa trước
            //bat 'docker stop myapp || true'
            //bat 'docker rm myapp || true'
            // 2. build image
            bat 'docker run -d --name myapp -p 8084:8080 myapp:latest'
            // bat 'docker-compose up -d'
        }
    }
}
