import akka.Done;
import akka.actor.ActorSystem;
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
    public void runAuthGraph() throws Exception {

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


        Sink<Object, CompletionStage<Done>> sink1 = Sink.ignore();
        Sink<String, CompletionStage<List<String>>> sink2 = Sink.collect(Collectors.toList());



        final TestKit probe = new TestKit(system);

        RunnableGraph<CompletionStage<Set<String>>> authGraph = testGraph.getTransactionFromAuth(testAuth, tenderList, settlementList, system);

        CompletionStage<Set<String>> running = authGraph.run(system);

        final Set<String> result = running.toCompletableFuture().get(3, TimeUnit.SECONDS);

        List<String> expect = new ArrayList();
        expect.add("clientMatcher1");

        assertEquals(expect, result.stream().toList());
//        Thread.sleep(4000);
//        System.out.println("Running from tests!!!");

    }

}
