def sendMessage(String status, String color, String stageName, String slackChannel, String errorMessage = "", String summary = "") {
    def message = ""
    switch (status) {
        case "start":
            message = ":rocket: Stage *${stageName}* started in job *${env.JOB_NAME}* (Build #${env.BUILD_NUMBER})"
            break
        case "success":
            message = ":white_check_mark: Stage *${stageName}* completed successfully"
            if (summary) {
                message += "\n\n*Details:*\n${summary}"
            }
            break
        case "failure":
            message = ":x: Stage *${stageName}* failed\n*Reason:* `${errorMessage}`"
            break
    }

    slackSend channel: slackChannel,
              color: color,
              message: message
}

def notifyStage(String stageName, String slackChannel, Closure body) {
    sendMessage("start", "#439FE0", stageName, slackChannel)

    def summary = "" // Initialize the summary variable outside the try-catch block

    try {
        // Using 'input' for user approval process
        // Note: 'input' will pause the pipeline and expect user interaction
        summary = body() 
    } catch (err) {
        sendMessage("failure", "danger", stageName, slackChannel, err.getMessage())
        throw err
    } finally {
        // Regardless of success or failure, send the final message
        if (summary) {
            sendMessage("success", "good", stageName, slackChannel, "", summary)
        } else {
            // If no summary is returned, send success message with a generic note
            sendMessage("success", "good", stageName, slackChannel)
        }
    }
}

// Example of how to use the `notifyStage` function
notifyStage("Approval Stage", "#my-channel") {
    // This is where you use 'input' for user approval
    input message: "Do you approve?", parameters: [choice(choices: ['Yes', 'No'], description: 'Approve?', name: 'approval')]

    // Return a summary after approval is received
    return "User approved"
}

