package opstree.common

import opstree.common.*

def notification_factory(Map step_params) {
    logger = new logger()
    if (step_params.notification_enabled == 'true') {
        notification(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Notification. Please mention correct values.', 'level':'WARN')
  }
}

def notification(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Notification', 'level':'INFO')
    build_status = "${step_params.build_status}"
    notification_channel = "${step_params.notification_channel}"

    if (notification_channel == 'teams') {
        def message = ''
        def color = ''
        def triggeredBy = currentBuild.getBuildCauses().find { cause -> cause instanceof hudson.model.Cause.UserIdCause }?.userId ?: (env.BUILD_USER_ID ?: "SCM Trigger/Unknown")
        def remarks = "Started by user ${triggeredBy}."
        def webhook_url_creds_id = "${step_params.webhook_url_creds_id}"

        switch (build_status) {
            case 'SUCCESS':
                message = "${env.JOB_NAME}: BUILD SUCCESS."
                color = '#008000' // Green
                break
            case 'FAILURE':
                message = "${env.JOB_NAME}: BUILD FAILED!!!"
                color = '#FF0000' // Red
                break
            case 'UNSTABLE':
                message = "${env.JOB_NAME}: BUILD UNSTABLE!!"
                color = '#FFFF00' // Yellow
                break
            default:
                message = "${env.JOB_NAME}: BUILD RESULT UNKNOWN!!"
                color = '#FFA500' // Orange
        }

        withCredentials([string(credentialsId: webhook_url_creds_id, variable: 'WEBHOOK_URL')]) {
            office365ConnectorSend(
                webhookUrl: "${WEBHOOK_URL}",
                status: build_status,
                color: color,
                message: """<b>${message}</b><br><br>
                          <strong>Build No: #${env.BUILD_NUMBER}</strong><br></br>
                          <strong>Remarks</strong>: ${remarks}<br><br>"""
            )
        }
    }

    if (notification_channel == 'slack') {
        def slack_channel = "${step_params.slack_channel}"
        def triggeredBy = currentBuild.getBuildCauses().find { cause -> cause instanceof hudson.model.Cause.UserIdCause }?.userId ?: (env.BUILD_USER_ID ?: "SCM Trigger/Unknown")
        def jobStartTime = new Date(currentBuild.startTimeInMillis).format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('Asia/Kolkata'))
        def message = "Job Build status: ${build_status}"
        def color

        switch (build_status) {
            case 'SUCCESS':
                message = "Job Build Successfully Completed."
                color = 'good'
                break
            case 'FAILURE':
                message = "Job Build Failed!!!!"
                color = 'danger'
                break
            case 'ABORTED':
                message = "Job Build was Aborted"
                color = 'warning'
                break
            default:
                color = 'warning'
        }

        slackSend(
            channel: slack_channel,
            color: color,
            message: """${message}
                      |Status: ${build_status}
                      |Triggered By: ${triggeredBy}
                      |Job Name: ${env.JOB_NAME}
                      |Build Number: ${env.BUILD_NUMBER}
                      |Build URL: ${env.BUILD_URL}
                      |Start Time: ${jobStartTime}""".stripMargin()
        )
    }
}
