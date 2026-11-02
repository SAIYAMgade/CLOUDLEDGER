variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "environment" {
  type    = string
  default = "production"
}

variable "private_subnet_ids" {
  type    = list(string)
  default = ["subnet-11111111", "subnet-22222222"]
}

variable "security_group_id" {
  type    = string
  default = "sg-0000000000"
}

variable "db_username" {
  type      = string
  default   = "cloudledgeradmin"
  sensitive = true
}

variable "db_password" {
  type      = string
  sensitive = true
}
