import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ClosedShape;
import akka.stream.Outlet;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import models.*;

public class AuthGraphDSL {
    public static void main(String[] args){
        System.out.println("Hello World from financial DSL");
    }

//    List<Transaction>
    void getTransactionFromAuth(Auth auth, List<Tender> tenders, List<Settlement> settlements, ActorSystem system){
        RunnableGraph.fromGraph(
                GraphDSL.create(
                        Sink.ignore(),
                        (builder,out) -> {
                            final Outlet<String> source = builder.add(Source.single(auth.paymentId())).out();
                            final UniformFanOutShape<String, String> paymentIdBCast = builder.add(Broadcast.create(2));

                            final Flow<String, String, NotUsed> tenderInitial =
                                    tenderRetrieveFlow(tenders, (Tender tender, String key) -> {return tender.paymentId() == key;}, system)
                                            .map(x -> x.clientMatcher());

                            final Flow<String, String, NotUsed> settlementInitial =
                                    settlementRetrieveFlow(settlements, (Settlement settlement, String key) -> {return settlement.paymentId() == key;}, system)
                                            .map(x -> x.clientMatcher());

                            builder.from(source).viaFanOut(paymentIdBCast);
                            builder.from(paymentIdBCast.out(0)).via(builder.add(tenderInitial));
                            builder.from(paymentIdBCast.out(1)).via(builder.add(settlementInitial));

                            return ClosedShape.getInstance();
                        }
                )
        );

//        Source.single(auth.paymentId())
//                .via(tenderRetrieveFlow(tenders, (tender, key) -> {return tender.paymentId() == key;}, system));

    }

    Flow<String, Tender, NotUsed> tenderRetrieveFlow(List<Tender> tenders, BiFunction<Tender, String, Boolean> bi, ActorSystem system){
        return Flow.of(String.class).flatMapConcat(key ->
                {
                    CompletionStage<List<Tender>> getMatchingTenders =  Source
                            .from(tenders)
                            .filter(tender -> bi.apply(tender, key))
                            .runWith(Sink.collect(Collectors.toList()), system);
                    return Source.from(getMatchingTenders.toCompletableFuture().get());
                });
    }

    Flow<String, Settlement, NotUsed> settlementRetrieveFlow(List<Settlement> settlements, BiFunction<Settlement, String, Boolean> bi, ActorSystem system){
        return Flow.of(String.class).flatMapConcat(key ->
        {
            CompletionStage<List<Settlement>> getMatchingSettlement =  Source
                    .from(settlements)
                    .filter(settlement -> bi.apply(settlement, key))
                    .runWith(Sink.collect(Collectors.toList()), system);
            return Source.from(getMatchingSettlement.toCompletableFuture().get());
        });
    }
}


