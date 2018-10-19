package com.star.sync.elasticsearch.scheduling;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.EventType;
import com.alibaba.otter.canal.protocol.Message;
import com.veelur.sync.elasticsearch.event.VerDeleteCanalEvent;
import com.veelur.sync.elasticsearch.event.VerInsertCanalEvent;
import com.veelur.sync.elasticsearch.event.VerUpdateCanalEvent;
import com.veelur.sync.elasticsearch.worker.BasicWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author <a href="mailto:wangchao.star@gmail.com">wangchao</a>
 * @version 1.0
 * @since 2017-08-26 22:44:00
 */
@Component
@ConditionalOnExpression("${thread.schedul-active:true}")
public class CanalScheduling extends BasicWorker implements Runnable, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(CanalScheduling.class);
    private ApplicationContext applicationContext;

    @Resource
    private CanalConnector canalConnector;

    @Value("${canal.batch.size:1000}")
    private int canalBatchSize;
    /**
     * 定时执行处理时间
     */
    private static final long schduleTime = 100;
    /**
     * schdule保证10秒后执行
     */
    private int schduleCount = (int) (1000 * 10 / schduleTime);
    /**
     * 初始应用标志
     */
    private int delaySign = 0;

    private boolean zkPathNode = false;

    @Scheduled(fixedDelay = schduleTime)
    @Override
    public void run() {
        if (!zkPathNode) {
            if (delaySign % schduleCount != 0) {
                //延迟1分钟执行
                delaySign++;
                return;
            } else if (!checkZookeeper()) {
                delaySign = 1;
                return;
            }
            zkPathNode = true;
            // 指定filter，格式 {database}.{table}，这里不做过滤，过滤操作留给用户
            canalConnector.subscribe();
            // 回滚寻找上次中断的位置
            canalConnector.rollback();
        }
        try {
//            Message message = connector.get(batchSize);
            Message message = canalConnector.getWithoutAck(canalBatchSize);
            long batchId = message.getId();
            try {
                List<Entry> entries = message.getEntries();
                if (batchId != -1 && entries.size() > 0) {
                    entries.forEach(entry -> {
                        if (entry.getEntryType() == EntryType.ROWDATA) {
                            publishCanalEvent(entry);
                        }
                    });
                    logger.info("获取binlog信息条数" + entries.size());
                }
                canalConnector.ack(batchId);
            } catch (Exception e) {
                logger.error("发送监听事件失败！batchId回滚,batchId=" + batchId, e);
                canalConnector.rollback(batchId);
            }
        } catch (Exception e) {
            logger.error("canal_scheduled异常！", e);
        }
    }

    private void publishCanalEvent(Entry entry) {
        EventType eventType = entry.getHeader().getEventType();
        switch (eventType) {
            /*case INSERT:
                applicationContext.publishEvent(new VerInsertCanalEvent(entry));
                break;
            case UPDATE:
                applicationContext.publishEvent(new VerUpdateCanalEvent(entry));
                break;
            case DELETE:
                applicationContext.publishEvent(new VerDeleteCanalEvent(entry));
                break;*/
            case INSERT:
                applicationContext.publishEvent(new VerInsertCanalEvent(entry));
                break;
            case UPDATE:
                applicationContext.publishEvent(new VerUpdateCanalEvent(entry));
                break;
            case DELETE:
                applicationContext.publishEvent(new VerDeleteCanalEvent(entry));
                break;
            default:
                break;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
