#!/bin/bash

echo "========================================"
echo "Delivery API Database Reset Script"
echo "========================================"
echo

# Check if DB_USERNAME is set, if not use default
if [ -z "$DB_USERNAME" ]; then
    echo "DB_USERNAME not set. Using default: postgres"
    DB_USERNAME=postgres
else
    echo "Using DB_USERNAME from environment: $DB_USERNAME"
fi

# Check if DB_PASSWORD is set, if not use default
if [ -z "$DB_PASSWORD" ]; then
    echo "DB_PASSWORD not set. Using default: postgres"
    DB_PASSWORD=postgres
else
    echo "Using DB_PASSWORD from environment: [SET]"
fi

echo
echo "Database credentials:"
echo "Username: $DB_USERNAME"
echo "Password: [HIDDEN]"
echo

# Ask for confirmation
read -p "This will DELETE ALL DATA in the database. Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo
    echo "Operation cancelled."
    exit 0
fi

echo
echo "Resetting database..."
docker exec -i delivery-postgres psql -U $DB_USERNAME -d deliverydb < reset-db.sql

if [ $? -eq 0 ]; then
    echo
    echo "========================================"
    echo "Database reset completed successfully!"
    echo "All tables have been cleared."
    echo "========================================"
else
    echo
    echo "========================================"
    echo "Error: Database reset failed!"
    echo "Please check your PostgreSQL connection."
    echo "Make sure PostgreSQL is running and credentials are correct."
    echo "========================================"
fi