job("Build") {
    container("openjdk:11.0.10-jdk-buster") {
        shellScript {
            interpreter = "/bin/bash"

            content = """
                apt update
                apt install -y libappindicator1 fonts-liberation
                wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
                dpkg -i google-chrome*.deb
                ./gradlew --no-daemon build
            """.trimIndent()
        }
    }
}