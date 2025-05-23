package dev.jchejarla.springbatch.clustering.polling;

public record PartitionAssignmentTask(Long jobExecutionId, String stepName, Long stepExecutionId, Long masterStepExecutionId, boolean isTransferable, String masterStepName, String assignedNode) {
}
