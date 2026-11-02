terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# 1. VPC and Subnets
resource "aws_vpc" "cloudledger_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "cloudledger-vpc"
    Environment = var.environment
  }
}

# 2. RDS PostgreSQL Multi-AZ Database
resource "aws_db_subnet_group" "rds_subnet_group" {
  name       = "cloudledger-rds-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "CloudLedger RDS Subnet Group"
  }
}

resource "aws_db_instance" "postgres" {
  identifier             = "cloudledger-postgres-authoritative"
  engine                 = "postgres"
  engine_version         = "16.3"
  instance_class         = "db.t4g.large"
  allocated_storage      = 100
  max_allocated_storage  = 500
  storage_type           = "gp3"
  db_name                = "cloudledger"
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.rds_subnet_group.name
  skip_final_snapshot    = true
  multi_az               = true
  publicly_accessible    = false
  deletion_protection    = true

  tags = {
    Name = "CloudLedger-Authoritative-Postgres"
  }
}

# 3. Amazon ElastiCache Redis for Idempotency Locks
resource "aws_elasticache_subnet_group" "redis_subnet_group" {
  name       = "cloudledger-redis-subnet-group"
  subnet_ids = var.private_subnet_ids
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id          = "cloudledger-redis-cluster"
  description                   = "Distributed locks and rate limiting for CloudLedger"
  node_type                     = "cache.t4g.medium"
  num_cache_clusters            = 2
  automatic_failover_enabled    = true
  engine                        = "redis"
  engine_version                = "7.1"
  port                          = 6379
  subnet_group_name             = aws_elasticache_subnet_group.redis_subnet_group.name
  at_rest_encryption_enabled    = true
  transit_encryption_enabled    = true

  tags = {
    Name = "CloudLedger-Redis"
  }
}

# 4. Amazon MSK (Managed Streaming for Apache Kafka)
resource "aws_msk_cluster" "kafka_cluster" {
  cluster_name           = "cloudledger-msk-cluster"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type   = "kafka.m5.large"
    client_subnets  = var.private_subnet_ids
    security_groups = [var.security_group_id]
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  tags = {
    Name = "CloudLedger-MSK-EventStreaming"
  }
}

# 5. ECS Fargate Cluster for Microservices
resource "aws_ecs_cluster" "cloudledger_cluster" {
  name = "cloudledger-ecs-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# 6. S3 Bucket for Financial Export Archive
resource "aws_s3_bucket" "report_exports" {
  bucket        = "cloudledger-reports-vault-${var.environment}"
  force_destroy = false

  tags = {
    Name = "CloudLedger Reports Vault"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "s3_enc" {
  bucket = aws_s3_bucket.report_exports.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}
