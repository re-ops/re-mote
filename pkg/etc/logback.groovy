import ch.qos.logback.core.FileAppender 
import static ch.qos.logback.classic.Level.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

appender('console', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
	pattern = "%level %logger [%thread] - %msg%n"
    }
}

appender('file', FileAppender) {
  file = 'third-party.log'
  append = true
  encoder(PatternLayoutEncoder) {
	pattern = "%level %logger [%thread] - %msg%n"
  }
}

logger('net.schmizz.sshj.transport.verification.OpenSSHKnownHosts',ERROR, ['file'])
logger('net.schmizz.sshj.common.SecurityUtils.BouncyCastleRegistration',INFO, ['file'])

root(INFO,['file'])
