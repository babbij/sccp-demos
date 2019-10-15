#!/bin/bash
# loads the Northwind files in to the RDF endpoint

cd /app
java -cp "/*:dependency/*" com.colabriq.demo.northwind.NorthwindLoader
