#!/bin/bash
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJkZWxpdmVyeS1hcGkiLCJzdWIiOiI4YjBlNjg3OC1mZjU3LTQwZTYtODAwNC0xN2NjN2VlZmUwNjgiLCJpYXQiOjE3NjMxMDY5NTksImV4cCI6MTc2Mzk3MDk1OSwicHJvdmlkZXIiOiJERVYiLCJ1c2VybmFtZSI6ImFkbWluIiwidHlwZSI6ImFjY2VzcyJ9.-1eIiyVkJ_wxNIv7ASD9AXduo6UUlrdkg57y6a7IgKA"
echo "Getting company info..."
curl -s "http://localhost:8081/api/companies/my" -H "Authorization: Bearer $TOKEN"
echo -e "\n\nCreating delivery..."
curl -s -X POST "http://localhost:8081/api/deliveries" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"receiverPhone":"012345678","receiverName":"Test","deliveryType":"COMPANY","itemDescription":"Test","pickupAddress":"123","deliveryAddress":"456","deliveryProvince":"PP","deliveryDistrict":"CM"}'
