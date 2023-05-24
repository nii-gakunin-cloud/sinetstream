#!/bin/bash

workdir=$(mktemp -d)
trap 'rm -rf $workdir' EXIT

setup_policy() {
policy=$workdir/policy.json
cat > $policy <<'EOS'
{
  "Statement": [
    {
      "Action": [
        "s3:GetBucketLocation",        
        "s3:ListBucket",
        "s3:ListBucketMultipartUploads"
      ],
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "*"
        ]
      },
      "Resource": [
        "arn:aws:s3:::sensor-data"        
      ]
    },    {
      "Action": [
        "s3:GetObject",
        "s3:ListMultipartUploadParts", 
        "s3:AbortMultipartUpload"      
      ],
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "*"
        ]
      },
      "Resource": [
        "arn:aws:s3:::sensor-data/*"      
      ]
    }
  ],
  "Version": "2012-10-17"
}
EOS

mc anonymous set-json $policy minio/sensor-data
}

if wait-for-it.sh localhost:9000 -t 60 ; then
    mc alias set minio http://localhost:9000 minioadmin minioadmin
    mc mb minio/sensor-data
    setup_policy
fi
