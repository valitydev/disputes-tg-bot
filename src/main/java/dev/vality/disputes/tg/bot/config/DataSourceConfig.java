package dev.vality.disputes.tg.bot.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableScheduling
public class DataSourceConfig {

    @Autowired
    private DataSource dataSource;

    @Scheduled(fixedRate = 30000) // каждые 30 секунд
    public void logConnectionPoolStats() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            try {
                var poolMXBean = hikariDataSource.getHikariPoolMXBean();
                int totalConnections = poolMXBean.getTotalConnections();
                int activeConnections = poolMXBean.getActiveConnections();
                int idleConnections = poolMXBean.getIdleConnections();
                int waitingThreads = poolMXBean.getThreadsAwaitingConnection();
                
                log.info("HikariCP Pool Stats - Total: {}, Active: {}, Idle: {}, Waiting: {}",
                        totalConnections, activeConnections, idleConnections, waitingThreads);
                
                // Предупреждение при высоком использовании пула
                if (activeConnections > totalConnections * 0.8) {
                    log.warn("High connection pool usage detected: {}/{} connections active", 
                            activeConnections, totalConnections);
                }
                
                // Предупреждение при ожидающих потоках
                if (waitingThreads > 0) {
                    log.warn("Threads waiting for connections: {}", waitingThreads);
                }
                
            } catch (Exception e) {
                log.warn("Failed to log connection pool stats", e);
            }
        }
    }
} 