package opstree.ci.template.java_ci

import opstree.common.build_dockerfile

class java_ci {
    
    def call(Map params) {
        println "[java_ci] Starting CI pipeline with params: ${params}"
        
        try {
            // Add TeamCity progress messages
            println "##teamcity[progressMessage 'Starting Java CI Pipeline...']"
            
            // VCS Checkout logs - TeamCity handles this automatically for the main repo
            println "##teamcity[progressMessage 'VCS checkout completed']"
            
            // Build Docker image step
            if (params.perform_build_dockerfile) {
                println "##teamcity[progressMessage 'Building Docker image...']"

                // Create an instance of the build_dockerfile class
                def dockerBuilder = new build_dockerfile()

                // Pass the parameters directly to the build_factory method
                dockerBuilder.build_factory([
                    perform_build_dockerfile: params.perform_build_dockerfile,
                    repo_url               : params.repo_https_url,  // URL to the repo containing the Dockerfile
                    image_name             : params.image_name,
                    source_code_path       : params.source_code_path,
                    dockerfile_context     : params.dockerfile_context,
                    dockerfile_location    : params.dockerfile_location,
                ])
                
                println "##teamcity[progressMessage 'Docker image build completed']"
            }
            
            println "[java_ci] Pipeline completed successfully"
            
        } catch (Exception e) {
            // Proper TeamCity message escaping for apostrophes
            def escapedMessage = e.message.replace("'", "|'")
            println "##teamcity[buildProblem description='CI Pipeline failed: ${escapedMessage}']"
            throw e
        }
    }
}
