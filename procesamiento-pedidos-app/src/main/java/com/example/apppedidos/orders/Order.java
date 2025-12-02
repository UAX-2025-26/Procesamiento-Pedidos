package com.example.apppedidos.orders;

// Representa un pedido con la información mínima necesaria para el ejemplo
public record Order(
        Long id,          // Identificador único del pedido
        double total,     // Importe total del pedido
        String customerName // Nombre del cliente asociado al pedido
) {
}
