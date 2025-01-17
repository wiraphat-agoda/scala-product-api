package com.example.internal.adapter.handler.product

import com.example.internal.core.product.port.ProductService
import com.example.internal.core.product.entity.Product
import cats.effect.IO
import org.http4s.{HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import io.circe.syntax._
import org.http4s.server.Router

class ProductHandler(productService: ProductService) extends Http4sDsl[IO] {
  import Product.{encoder, decoder}

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      for {
        products <- productService.getAllProducts
        response <- Ok(products.asJson)
      } yield response

    case GET -> Root / LongVar(id) =>
      for {
        maybeProduct <- productService.getProduct(id)
        response <- maybeProduct.fold(NotFound())(product => Ok(product.asJson))
      } yield response

    case req @ POST -> Root =>
      for {
        product <- req.as[Product]
        created <- productService.createProduct(product)
        response <- Created(created.asJson)
      } yield response

    case req @ PUT -> Root / LongVar(id) =>
      for {
        product <- req.as[Product]
        updated <- productService.updateProduct(id, product)
        response <- updated.fold(NotFound())(product => Ok(product.asJson))
      } yield response

    case DELETE -> Root / LongVar(id) =>
      for {
        deleted <- productService.deleteProduct(id)
        response <- if (deleted) Ok() else NotFound()
      } yield response
  }

  val productRouter: HttpRoutes[IO] = Router(
    "/products" -> this.routes
  )
}