/**
 * DoualaDirect implements the Sherlok protocol. 
 * Sherlok receives ACII commands and sends the response as XML stream. 
 * DoualaDirect maps each XML element to a JTabbedPane by name. 
 * Sub-elements are represented in detail views on the same page.
 * 
 * Each XML block is transferred synchronously. The components do not block during 
 * transfer, but reject pending updates
 * 
 */
package com.sap.douala;

