import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ClosedShape;
import akka.stream.Outlet;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jdk.jshell.JShell;
import models.*;

public class AuthGraphDSL {
    public static void main(String[] args){
        System.out.println("Hello World from financial DSL");
    }

//    List<Transaction>
    RunnableGraph<CompletionStage<Set<String>>> getTransactionFromAuth(Auth auth, List<Tender> tenders, List<Settlement> settlements, ActorSystem system){

        final Sink<String, CompletionStage<Set<String>>> allSink = Sink.collect(Collectors.toSet());
        final RunnableGraph<CompletionStage<Set<String>>> result =
        RunnableGraph.fromGraph(
                GraphDSL.create(
                        allSink,
                        (builder,out) -> {

                            final Outlet<String> source = builder.add(Source.single(auth.paymentId())).out();

                            final UniformFanOutShape<String, String> paymentIdBCast = builder.add(Broadcast.create(2));

                            final Flow<String, String, NotUsed> tenderInitial =
                                    tenderRetrieveFlow(tenders, (Tender tender, String key) -> {
                                        System.out.println("checking tender: " + tender.paymentId() + " key: " + key);
                                        return tender.paymentId() == key;}, system)
                                            .map(x -> x.clientMatcher());

                            final Flow<String, String, NotUsed> settlementInitial =
                                    settlementRetrieveFlow(settlements, (Settlement settlement, String key) -> settlement.paymentId() == key, system)
                                            .map(x -> x.clientMatcher());

                            final UniformFanInShape<String, String> merge = builder.add(Merge.create(2));

                            builder.from(source).viaFanOut(paymentIdBCast);

                            builder.from(paymentIdBCast.out(0)).via(builder.add(tenderInitial)).toInlet(merge.in(0));
                            builder.from(paymentIdBCast.out(1)).via(builder.add(settlementInitial)).toInlet(merge.in(1));

                            builder.from(merge).via(builder.add(
                                    Flow.of(String.class).map(x -> {
                                        System.out.println("Received: " + x);
                                        return x;}
                                    )
                            )).to(out);

//                            builder.from(merge).to(out);

                            return ClosedShape.getInstance();
                        }
                )
        );

        return result;

//        Source.single(auth.paymentId())
//                .via(tenderRetrieveFlow(tenders, (tender, key) -> {return tender.paymentId() == key;}, system));

    }

    Flow<String, Tender, NotUsed> tenderRetrieveFlow(List<Tender> tenders, BiFunction<Tender, String, Boolean> bi, ActorSystem system){
        return Flow.of(String.class).flatMapConcat(key ->
                {
                    CompletionStage<List<Tender>> getMatchingTenders =  Source
                            .from(tenders)
                            .map(x -> {
                                System.out.println("Running from inside tender");
                                return x;})
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


