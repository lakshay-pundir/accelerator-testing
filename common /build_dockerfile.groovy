package opstree.common

class build_dockerfile {
    def build_factory(Map params) {
        def repoUrl = params.repo_url
        def imageName = params.image_name
        def dockerfileContext = params.dockerfile_context ?: "."
        def dockerfileLocation = params.dockerfile_location ?: "Dockerfile"
        
        // Create a temporary directory for cloning the repo with Dockerfile
        def tempDir = File.createTempDir("dockerfile-repo", "")
        tempDir.deleteOnExit()
        
        try {
            // Clone the repository containing the Dockerfile
            println("Cloning repository: ${repoUrl}")
            def cloneCommand = ["git", "clone", "--depth", "1", repoUrl, tempDir.absolutePath]
            def cloneProcess = new ProcessBuilder(cloneCommand)
                .redirectErrorStream(true)
                .start()
            
            // Capture and print output
            def cloneReader = new BufferedReader(new InputStreamReader(cloneProcess.getInputStream()))
            String cloneLine
            while ((cloneLine = cloneReader.readLine()) != null) {
                println(cloneLine)
            }
            
            def cloneExitCode = cloneProcess.waitFor()
            if (cloneExitCode != 0) {
                throw new RuntimeException("Failed to clone repository: ${repoUrl}")
            }
            
            // Check if Dockerfile exists
            def dockerfilePath = new File(tempDir, dockerfileLocation)
            if (!dockerfilePath.exists()) {
                throw new RuntimeException("Dockerfile not found at path: ${dockerfileLocation}")
            }
            
            // Build the Docker image
            println("Starting Docker build process...")
            def dockerBuildArgs = ["docker", "build", "-t", imageName, "-f", dockerfileLocation, tempDir.absolutePath]
            
            println("Running Docker build command: ${dockerBuildArgs.join(' ')}")
            def buildProcess = new ProcessBuilder(dockerBuildArgs)
                .redirectErrorStream(true)
                .directory(tempDir)
                .start()
            
            // Capture and print output
            def buildReader = new BufferedReader(new InputStreamReader(buildProcess.getInputStream()))
            String buildLine
            while ((buildLine = buildReader.readLine()) != null) {
                println(buildLine)
            }
            
            def exitCode = buildProcess.waitFor()
            if (exitCode != 0) {
                throw new RuntimeException("Docker build failed with exit code: ${exitCode}")
            }
            
            return true
        } catch (Exception e) {
            // Handling errors if the build fails
            println("Error during Docker build: ${e.message}")
            // Correct usage of the TeamCity service message
            println("##teamcity[buildProblem description='Docker build failed: ${e.message.replace("'", "|'")}']")
            throw e // Re-throw to fail the build
        } finally {
            // Clean up - delete the temporary directory
            if (tempDir.exists()) {
                tempDir.deleteDir()
            }
        }
    }
}
