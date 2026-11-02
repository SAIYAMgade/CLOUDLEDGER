output "rds_endpoint" {
  description = "Authoritative PostgreSQL RDS Multi-AZ Endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "redis_primary_endpoint" {
  description = "ElastiCache Redis Primary Endpoint"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "msk_bootstrap_brokers_tls" {
  description = "MSK TLS Bootstrap Brokers"
  value       = aws_msk_cluster.kafka_cluster.bootstrap_brokers_tls
}

output "s3_vault_bucket" {
  description = "S3 Financial Report Exports Bucket"
  value       = aws_s3_bucket.report_exports.bucket
}
