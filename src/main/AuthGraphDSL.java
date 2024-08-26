import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.japi.Pair$;
import akka.stream.ClosedShape;
import akka.stream.Outlet;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.*;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jdk.jshell.JShell;
import models.*;
import org.mockito.internal.matchers.Not;

public class AuthGraphDSL {
    public static void main(String[] args){
        System.out.println("Hello World from financial DSL");
    }

    RunnableGraph<CompletionStage<List<TransactionType>>> getTransactionFromAuth(Auth auth, List<Tender> tenders, List<Settlement> settlements, ActorSystem system){

        final Sink<TransactionType, CompletionStage<List<TransactionType>>> allSink = Sink.collect(Collectors.toList());

        final RunnableGraph<CompletionStage<List<TransactionType>>> result =
        RunnableGraph.fromGraph(
                GraphDSL.create(
                        allSink,
                        (builder,out) -> {

                            final Outlet<String> source = builder.add(Source.single(auth.paymentId())).out();

                            final UniformFanOutShape<String, String> paymentIdBCast = builder.add(Broadcast.create(2));

                            final Flow<String, String, NotUsed> tenderInitial =
                                    tenderRetrieveFlow(tenders, (Tender tender, String key) -> tender.paymentId().equals(key), system)
                                            .map(Tender::clientMatcher);

                            final Flow<String, String, NotUsed> settlementInitial =
                                    settlementRetrieveFlow(settlements, (Settlement settlement, String key) -> settlement.paymentId().equals(key), system)
                                            .map(Settlement::clientMatcher);

                            final UniformFanInShape<String, String> merge1 = builder.add(Merge.create(2));


                            final Flow<String,String, NotUsed> deduplicate =
                                    Flow.of(String.class).statefulMapConcat(

                                            () -> {
                                                List<String> passed = new ArrayList<>();

                                                return (String str1) -> {
                                                    if(!passed.contains(str1)) {
                                                        passed.add(str1);
                                                        return Collections.singletonList(str1);
                                                    }
                                                    return Collections.emptyList();
                                                };
                                            }
                                    );

                            final UniformFanOutShape<String, String> paymentIdStage2BCast = builder.add(Broadcast.create(2));

                            final Flow<String, Settlement, NotUsed> settlementFinal =
                                    settlementRetrieveFlow(settlements, (Settlement settlement, String key) -> settlement.clientMatcher().equals(key), system);

                            final Flow<String, Tender, NotUsed> tenderFinal =
                                    tenderRetrieveFlow(tenders, (Tender tender, String key) -> tender.clientMatcher().equals(key), system);

                            final UniformFanInShape<TransactionType, TransactionType> merge2 = builder.add(Merge.create(2));

//                            final Flow<String, Pair<Settlement, Settlement>, NotUsed> settlementFinal = settlementRetrieveFlow(settlements, (Settlement settlement, String key) -> settlement.clientMatcher() == key, system)
//                                    .map(x -> new Pair<>(x,x));
//
//                            final Flow<Pair<Settlement, Settlement>, Pair<Settlement, Settlement>, NotUsed> flowPairSettlement = Flow.create();
//
//                            final FlowWithContext<Settlement, Settlement, String, Settlement, NotUsed> afterSettlementFinal =
//                                    flowPairSettlement.<Settlement,Settlement, Settlement>asFlowWithContext(Pair::create, Pair::second)
//                                            .map(Pair::first)
//                                            .map(x -> x.paymentId());
//
//                            final Flow<String, Pair<Tender, Tender>, NotUsed> tenderFinal = tenderRetrieveFlow(tenders, (Tender tender, String key) -> tender.clientMatcher() == key, system)
//                                    .map(x -> new Pair<>(x,x));
//
//                            final Flow<Pair<Tender, Tender>, Pair<Tender, Tender>, NotUsed> flowPairTender = Flow.create();
//
//                            final FlowWithContext<Tender, Tender, String, Tender, NotUsed> afterTenderFinal =
//                                    flowPairTender.<Tender,Tender, Tender>asFlowWithContext(Pair::create, Pair::second)
//                                            .map(Pair::first)
//                                            .map(x -> x.paymentId());

                            builder.from(source).viaFanOut(paymentIdBCast);

                            builder.from(paymentIdBCast.out(0)).via(builder.add(tenderInitial)).toInlet(merge1.in(0));
                            builder.from(paymentIdBCast.out(1)).via(builder.add(settlementInitial)).toInlet(merge1.in(1));

                            builder.from(merge1)
                                    .via(builder.add(deduplicate))
                                    .viaFanOut(paymentIdStage2BCast);

                            builder.from(paymentIdStage2BCast.out(0))

                                    .via(builder.add(tenderFinal))
                                    .toInlet(merge2.in(0));

                            builder.from(paymentIdStage2BCast.out(1)).via(builder.add(settlementFinal))
//                                    .via(builder.add(Flow.of(TransactionType.class).map(x -> {
//                                        System.out.println("In Settlement Stream: "+x);
//                                        return x;
//                                    })))
                                    .toInlet(merge2.in(1));

                            builder.from(merge2).to(out);

                            return ClosedShape.getInstance();
                        }
                )
        );

        return result;

    }

    Flow<String, Tender, NotUsed> tenderRetrieveFlow(List<Tender> tenders, BiFunction<Tender, String, Boolean> bi, ActorSystem system){


        return Flow.of(String.class).flatMapConcat(key ->
                {
                    CompletionStage<List<Tender>> getMatchingTenders =  Source
                            .from(tenders)
                            .filter(tender -> bi.apply(tender, key))
                            .runWith(Sink.collect(Collectors.toList()), system);
                    List<Tender> l = getMatchingTenders.toCompletableFuture().get();
                    return Source.from(l);
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


