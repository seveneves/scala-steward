package org.scalasteward.core.vcs.azurerepos

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import munit.FunSuite
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Uri}
import org.scalasteward.core.application.Config.AzureReposConfig
import org.scalasteward.core.git.Sha1.HexString
import org.scalasteward.core.git.{Branch, Sha1}
import org.scalasteward.core.util.HttpJsonClient
import org.scalasteward.core.vcs.data._

class AzureReposApiAlgTest extends FunSuite {
  private val repo = Repo("scala-steward-org", "scala-steward")
  private val apiHost = uri"https://dev.azure.com"

  object branchNameMatcher extends QueryParamDecoderMatcher[String]("name")
  object sourceRefNameMatcher
      extends QueryParamDecoderMatcher[String]("searchCriteria.sourceRefName")
  object targetRefNameMatcher
      extends QueryParamDecoderMatcher[String]("searchCriteria.targetRefName")

  private val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "azure-org" / repo.owner / "_apis/git/repositories" / repo.repo =>
      Ok("""
           |{
           |    "id": "3846fbbd-71a0-402b-8352-6b1b9b2088aa",
           |    "name": "scala-steward",
           |    "url": "https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward",
           |    "project": {
           |        "id": "2a608025-b9aa-4918-a306-5aae0a8b7458",
           |        "name": "scala-steward-org"
           |    },
           |    "defaultBranch": "refs/heads/main",
           |    "size": 7786160,
           |    "remoteUrl": "https://steward-user@dev.azure.com/azure-org/scala-steward-org/_git/scala-steward",
           |    "isDisabled": false
           |}""".stripMargin)

    case GET -> Root / "azure-org" / repo.owner / "_apis/git/repositories" / repo.repo / "stats/branches"
        :? branchNameMatcher("main") =>
      Ok("""
           |{
           |    "commit": {
           |        "commitId": "f55c9900528e548511fbba6874c873d44c5d714c"                          
           |    },
           |    "name": "main",
           |    "aheadCount": 0,
           |    "behindCount": 0,
           |    "isBaseVersion": true
           |}""".stripMargin)

    case POST -> Root / "azure-org" / repo.owner / "_apis/git/repositories" / repo.repo / "pullrequests" =>
      Created(
        """
          |{
          |  "repository": {
          |    "id": "3846fbbd-71a0-402b-8352-6b1b9b2088aa",
          |    "name": "scala-steward",
          |    "url": "https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward",
          |    "project": {
          |      "id": "a7573007-bbb3-4341-b726-0c4148a07853",
          |      "name": "scala-steward-org"
          |    },
          |    "remoteUrl": "https://steward-user@dev.azure.com/azure-org/scala-steward-org/_git/scala-steward"
          |  },
          |  "pullRequestId": 22,
          |  "status": "active",
          |  "creationDate": "2016-11-01T16:30:31.6655471Z",
          |  "title": "Update cats-effect to 3.3.14",
          |  "description": "Updates org.typelevel:cats-effect  from 3.3.13 to 3.3.14.",
          |  "sourceRefName": "refs/heads/update/cats-effect-3.3.14",
          |  "targetRefName": "refs/heads/main",
          |  "url": "https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward/pullRequests/22",
          |  "supportsIterations": true
          |}""".stripMargin
      )

    case GET -> Root / "azure-org" / repo.owner / "_apis/git/repositories" / repo.repo / "pullrequests" :?
        sourceRefNameMatcher("refs/heads/update/cats-effect-3.3.14")
        +& targetRefNameMatcher("refs/heads/main") =>
      Ok("""
           |{
           |   "value":[
           |      {
           |         "pullRequestId":26,
           |         "status":"active",
           |         "creationDate":"2022-07-24T16:58:51.0719239Z",
           |         "title":"Update cats-effect to 3.3.14",
           |         "description":"Updates [org.typelevel:cats-effect]",
           |         "sourceRefName":"refs/heads/update/cats-effect-3.3.14",
           |         "targetRefName":"refs/heads/main",
           |         "mergeStatus":"succeeded",
           |         "isDraft":false,
           |         "mergeId":"3ff8afa0-1147-4158-b215-74a0b5a2e162",
           |         "reviewers":[
           |            
           |         ],
           |         "url":"https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward/pullRequests/26",
           |         "supportsIterations":true
           |      }
           |   ],
           |   "count":1
           |}""".stripMargin)

    case PATCH -> Root / "azure-org" / repo.owner / "_apis/git/repositories" / repo.repo / "pullrequests" / "26" =>
      Ok("""
           |{
           |  "repository": {
           |    "id": "3846fbbd-71a0-402b-8352-6b1b9b2088aa",
           |    "name": "scala-steward",
           |    "url": "https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward",
           |    "project": {
           |      "id": "a7573007-bbb3-4341-b726-0c4148a07853",
           |      "name": "scala-steward-org"
           |    },
           |    "remoteUrl": "https://steward-user@dev.azure.com/azure-org/scala-steward-org/_git/scala-steward"
           |  },
           |  "pullRequestId": 26,
           |  "status": "abandoned",
           |  "creationDate": "2016-11-01T16:30:31.6655471Z",
           |  "title": "Update cats-effect to 3.3.14",
           |  "description": "Updates org.typelevel:cats-effect  from 3.3.13 to 3.3.14.",
           |  "sourceRefName": "refs/heads/update/cats-effect-3.3.14",
           |  "targetRefName": "refs/heads/main",
           |  "url": "https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward/pullRequests/26",
           |  "supportsIterations": true
           |}""".stripMargin)

    case POST -> Root / "azure-org" / repo.owner / "_apis/git/repositories" / repo.repo / "pullrequests" / "26" / "threads" =>
      Ok("""
           |{
           |   "id":17,
           |   "publishedDate":"2022-07-24T22:06:00.067Z",
           |   "lastUpdatedDate":"2022-07-24T22:06:00.067Z",
           |   "comments":[
           |      {
           |         "id":1,
           |         "parentCommentId":0,
           |         "content":"Test comment",
           |         "publishedDate":"2022-07-24T22:06:00.067Z",
           |         "lastUpdatedDate":"2022-07-24T22:06:00.067Z",
           |         "lastContentUpdatedDate":"2022-07-24T22:06:00.067Z"
           |      }
           |   ],
           |   "status":"active"
           |}""".stripMargin)

    case POST -> Root / "azure-org" / repo.owner / "_apis/git/repositories" / repo.repo / "pullrequests" / "26" / "labels" =>
      Ok("""
           |{
           |    "id": "921dbff4-9c00-49d6-9262-ab0d6e4a13f1",
           |    "name": "dependency-updates",
           |    "active": true,
           |    "url": "https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward/pullRequests/26/labels/921dbff4-9c00-49d6-9262-ab0d6e4a13f1"
           |}""".stripMargin)

  }

  implicit private val client: Client[IO] = Client.fromHttpApp(routes.orNotFound)
  implicit private val httpJsonClient: HttpJsonClient[IO] = new HttpJsonClient[IO]
  private val azureRepoCfg = AzureReposConfig(organization = Some("azure-org"))

  private val azureReposApiAlg: AzureReposApiAlg[IO] =
    new AzureReposApiAlg[IO](
      apiHost,
      azureRepoCfg,
      _ => IO.pure
    )

  test("getRepo") {
    val obtained = azureReposApiAlg.getRepo(repo).unsafeRunSync()
    val expected = RepoOut(
      "scala-steward",
      UserOut("scala-steward-org"),
      None,
      Uri.unsafeFromString(
        "https://steward-user@dev.azure.com/azure-org/scala-steward-org/_git/scala-steward"
      ),
      Branch("refs/heads/main")
    )
    assertEquals(obtained, expected)
  }

  test("getBranch") {
    val obtained = azureReposApiAlg.getBranch(repo, Branch("refs/heads/main")).unsafeRunSync()
    val expected = BranchOut(
      Branch("main"),
      CommitOut(Sha1(HexString.unsafeFrom("f55c9900528e548511fbba6874c873d44c5d714c")))
    )
    assertEquals(obtained, expected)
  }

  test("createPullRequest") {
    val obtained = azureReposApiAlg
      .createPullRequest(
        repo,
        NewPullRequestData(
          title = "Update cats-effect to 3.3.14",
          body = "Updates org.typelevel:cats-effect  from 3.3.13 to 3.3.14.",
          head = "refs/heads/update/cats-effect-3.3.14",
          base = Branch("refs/heads/main"),
          labels = List.empty
        )
      )
      .unsafeRunSync()

    val expected = PullRequestOut(
      uri"https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward/pullRequests/22",
      PullRequestState.Open,
      PullRequestNumber(22),
      "Update cats-effect to 3.3.14"
    )
    assertEquals(obtained, expected)
  }

  test("listPullRequests") {
    val obtained = azureReposApiAlg
      .listPullRequests(
        repo,
        "refs/heads/update/cats-effect-3.3.14",
        Branch("refs/heads/main")
      )
      .unsafeRunSync()

    val expected = List(
      PullRequestOut(
        uri"https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward/pullRequests/26",
        PullRequestState.Open,
        PullRequestNumber(26),
        "Update cats-effect to 3.3.14"
      )
    )
    assertEquals(obtained, expected)
  }

  test("closePullRequest") {
    val obtained = azureReposApiAlg.closePullRequest(repo, PullRequestNumber(26)).unsafeRunSync()
    val expected = PullRequestOut(
      uri"https://dev.azure.com/azure-org/scala-steward-org/_apis/git/repositories/scala-steward/pullRequests/26",
      PullRequestState.Closed,
      PullRequestNumber(26),
      "Update cats-effect to 3.3.14"
    )

    assertEquals(obtained, expected)
  }

  test("commentPullRequest") {
    val obtained = azureReposApiAlg
      .commentPullRequest(repo, PullRequestNumber(26), "Test comment")
      .unsafeRunSync()
    val expected = Comment("Test comment")

    assertEquals(obtained, expected)
  }

  test("labelPullRequest") {
    val obtained =
      azureReposApiAlg
        .labelPullRequest(repo, PullRequestNumber(26), List("dependency-updates"))
        .attempt
        .unsafeRunSync()
    assert(obtained.isRight)
  }
}
