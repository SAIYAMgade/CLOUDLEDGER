# CloudLedger AWS Cloud Architecture & Deployment Guide

## 1. AWS Topology Architecture

```mermaid
flowchart TD
    Users([Internet Clients]) --> CF[Amazon CloudFront CDN]
    CF --> S3Web[Amazon S3\nStatic Web Assets]
    
    Users --> ALB[Application Load Balancer]
    ALB --> ECS[ECS Fargate Tasks\nSpring Boot Backend]
    
    subgraph Private VPC Subnets
        ECS --> RDS[(Amazon RDS PostgreSQL\nMulti-AZ Primary & Standby)]
        ECS --> ElastiCache[(Amazon ElastiCache Redis\nCluster)]
        ECS --> MSK{{Amazon MSK\nApache Kafka Brokers}}
        ECS --> S3Vault[(Amazon S3 Vault\nEncrypted Financial CSV Exports)]
    end
    
    subgraph Observability
        ECS -.-> CW[Amazon CloudWatch Logs & Metrics]
        RDS -.-> CW
    end
```

## 2. Infrastructure as Code (Terraform)

The Terraform configuration in `infra/terraform/` defines:
- Multi-AZ Amazon RDS PostgreSQL 16 (`db.t4g.large`) with deletion protection and encrypted storage.
- Amazon ElastiCache Redis 7.1 replication group with in-transit and at-rest encryption.
- Amazon MSK Kafka 3.6.0 with TLS encryption.
- ECS Fargate cluster with Container Insights.
- S3 bucket for financial report exports encrypted with AWS KMS.

```bash
cd infra/terraform
terraform init
terraform plan
terraform apply
```
