package com.itcast.mq.consumer;

import com.itcast.constant.MqConstant;
import com.itcast.model.dto.NoteEsSyncMessage;
import com.itcast.model.pojo.NoteEs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NoteEsSyncConsumer {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.NOTE_ES_SYNC_QUEUE),
            exchange = @Exchange(name = MqConstant.NOTE_ES_EXCHANGE),
            key = MqConstant.NOTE_ES_SYNC_KEY
    ))
    public void handleNoteSync(NoteEsSyncMessage message) {
        log.info("Received Note ES Sync message: {}", message);
        try {
            if ("INSERT".equals(message.getType()) || "UPDATE".equals(message.getType())) {
                if (message.getNote() == null) {
                    log.error("Note is null for INSERT/UPDATE operation");
                    return;
                }
                NoteEs noteEs = new NoteEs();
                BeanUtils.copyProperties(message.getNote(), noteEs);
                
                // Ensure ID is set and converted to Integer as per NoteEs definition
                Long noteId = message.getNoteId() != null ? message.getNoteId() : message.getNote().getId();
                if (noteId != null) {
                    noteEs.setId(noteId);
                }
                
                elasticsearchOperations.save(noteEs);
                log.info("Successfully saved/updated note to ES, id: {}", noteEs.getId());
            } else if ("DELETE".equals(message.getType())) {
                if (message.getNoteId() == null) {
                    log.error("NoteId is null for DELETE operation");
                    return;
                }
                elasticsearchOperations.delete(String.valueOf(message.getNoteId()), NoteEs.class);
                log.info("Successfully deleted note from ES, id: {}", message.getNoteId());
            } else {
                log.warn("Unknown sync type: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("Failed to sync note to ES", e);
            throw e; // Throw exception to trigger retry
        }
    }
}
