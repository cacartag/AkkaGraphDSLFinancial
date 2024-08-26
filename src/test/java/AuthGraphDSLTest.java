import akka.Done;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.testkit.javadsl.TestKit;
import models.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static junit.framework.Assert.assertEquals;

public class AuthGraphDSLTest {

    @Test
    public void runAuthGraphScenario1() throws Exception {

        AuthGraphDSL testGraph = new AuthGraphDSL();
        ActorSystem system = ActorSystem.create("testActorSystem");

        Auth testAuth = new Auth("paymentId1", "Auth1");

        Tender testTender1 = new Tender("paymentId1", "clientMatcher1",
                "invoiceId1", "tenderPayload1");

        Settlement testSettlement1 = new Settlement("paymentId1", "invoiceId1",
                "clientMatcher1", "settlementPayload1");

        List<Tender> tenderList = new ArrayList();
        tenderList.add(testTender1);

        List<Settlement> settlementList = new ArrayList();
        settlementList.add(testSettlement1);

        RunnableGraph<CompletionStage<List<TransactionType>>> authGraph = testGraph.getTransactionFromAuth(testAuth, tenderList, settlementList, system);

        CompletionStage<List<TransactionType>> running = authGraph.run(system);

        final List<TransactionType> result = running.toCompletableFuture().get(3, TimeUnit.SECONDS);

//        List<String> expect = new ArrayList();
//        expect.add("clientMatcher1");

        List<TransactionType> expect = new ArrayList();
        expect.add(testTender1);
        expect.add(testSettlement1);

        System.out.println("Return length is: "+ result.size());

        result.stream().forEach(System.out::println);

        assert(result.contains(testTender1));
        assert(result.contains(testSettlement1));
//        assertEquals(expect, result.stream().toList());
    }

    @Test
    public void runAuthGraphScenario2() throws Exception {

//        AuthGraphDSL testGraph = new AuthGraphDSL();
//
//        ActorSystem system = ActorSystem.create("testActorSystem");
//
//        Auth testAuth = new Auth("paymentId1", "Auth1");
//
//        Tender testTender1 = new Tender("paymentId1", "clientMatcher1",
//                "invoiceId1", "tenderPayload1");
//        Tender testTender2 = new Tender("paymentId1", "clientMatcher2",
//                "invoiceId2", "tenderPayload1");
//        Tender testTender3 = new Tender("paymentId2", "clientMatcher4",
//                "invoiceId3", "tenderPayload4");
//        Tender testTender4 = new Tender("paymentId3", "clientMatcher5",
//                "invoiceId4", "tenderPayload5");
//
//        Settlement testSettlement1 = new Settlement("paymentId1", "invoiceId1",
//                "clientMatcher1", "settlementPayload1");
//
//        List<Tender> tenderList = new ArrayList();
//        tenderList.add(testTender1);
//        tenderList.add(testTender2);
//        tenderList.add(testTender3);
//        tenderList.add(testTender4);
//
//        List<Settlement> settlementList = new ArrayList();
//        settlementList.add(testSettlement1);
//
//        RunnableGraph<CompletionStage<Set<String>>> authGraph = testGraph.getTransactionFromAuth(testAuth, tenderList, settlementList, system);
//
//        CompletionStage<Set<String>> running = authGraph.run(system);
//
//        final Set<String> result = running.toCompletableFuture().get(3, TimeUnit.SECONDS);
//
//        List<String> expect = new ArrayList();
//        expect.add("clientMatcher1");
//        expect.add("clientMatcher2");
//
//        Pair<Settlement, Settlement> n = new Pair(testSettlement1, testSettlement1);
//
//        result.stream().forEach(System.out::println);
//
//        assertEquals(expect, result.stream().toList());

    }
}
