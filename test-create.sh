#!/bin/bash
curl -X POST http://localhost:8081/api/deliveries \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJkZWxpdmVyeS1hcGkiLCJzdWIiOiJmNjQyYjdiNi0zNTZhLTRlYmItOWExZi1iMTUyNDE5MzUxYWQiLCJpYXQiOjE3NjQ3NTYzNjMsImV4cCI6MTc2NTYyMDM2MywicHJvdmlkZXIiOiJMT0NBTCIsInVzZXJuYW1lIjoidV8rODU1ODk1MDQ0MDUiLCJ0eXBlIjoiYWNjZXNzIn0.-3gzFWoSkDZkALHWPzBGwN8vNTGv5weMwbVJca9FPns" \
  -d @create-delivery-payload-example.json
