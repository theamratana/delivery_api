#!/bin/bash
# Test script for Product Search API

API_BASE="http://localhost:8081/api"

echo "======================================"
echo "Product Search API Test"
echo "======================================"
echo ""

# Get a dev token
echo "📝 Getting dev token..."
TOKEN_RESPONSE=$(curl -s -X POST "$API_BASE/../auth/dev/token/new")
TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
USER_ID=$(echo "$TOKEN_RESPONSE" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to get token"
  exit 1
fi

echo "✅ Got token for user: $USER_ID"
echo ""

# Test 1: Search all products (default pagination)
echo "Test 1: GET /products/search (all products)"
echo "----------------------------------------"
curl -s -X GET "$API_BASE/products/search" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 2: Search with query parameter
echo "Test 2: GET /products/search?query=iPhone"
echo "----------------------------------------"
curl -s -X GET "$API_BASE/products/search?query=iPhone" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 3: Filter by published status
echo "Test 3: GET /products/search?published=true"
echo "----------------------------------------"
curl -s -X GET "$API_BASE/products/search?published=true" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 4: Pagination test
echo "Test 4: GET /products/search?page=0&limit=5"
echo "----------------------------------------"
curl -s -X GET "$API_BASE/products/search?page=0&limit=5" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 5: Combined filters
echo "Test 5: GET /products/search?query=phone&published=true&limit=10"
echo "----------------------------------------"
curl -s -X GET "$API_BASE/products/search?query=phone&published=true&limit=10" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 6: Get suggestions
echo "Test 6: GET /products/suggestions?query=iP"
echo "----------------------------------------"
curl -s -X GET "$API_BASE/products/suggestions?query=iP" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 7: Traditional search (backward compatibility)
echo "Test 7: GET /products?search=phone (backward compatibility)"
echo "----------------------------------------"
curl -s -X GET "$API_BASE/products?search=phone" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

echo "======================================"
echo "All tests completed!"
echo "======================================"
