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

//        final Source<Integer, NotUsed> in = Source.from(Arrays.asList(1, 2, 3, 4, 5));
//        final Sink<List<String>, CompletionStage<List<String>>> sink = Sink.head();
//        final Flow<Integer, Integer, NotUsed> f1 = Flow.of(Integer.class).map(elem -> elem + 10);
//        final Flow<Integer, Integer, NotUsed> f2 = Flow.of(Integer.class).map(elem -> elem + 20);
//        final Flow<Integer, String, NotUsed> f3 = Flow.of(Integer.class).map(elem -> elem.toString());
//        final Flow<Integer, Integer, NotUsed> f4 = Flow.of(Integer.class).map(elem -> elem + 30);

//        final RunnableGraph<CompletionStage<List<String>>> result =
//                RunnableGraph.fromGraph(
//                        GraphDSL // create() function binds sink, out which is sink's out port and builder DSL
//                                .create( // we need to reference out's shape in the builder DSL below (in to()
//                                        // function)
//                                        sink, // previously created sink (Sink)
//                                        (builder, out) -> { // variables: builder (GraphDSL.Builder) and out (SinkShape)
//                                            final UniformFanOutShape<Integer, Integer> bcast =
//                                                    builder.add(Broadcast.create(2));
//                                            final UniformFanInShape<Integer, Integer> merge = builder.add(Merge.create(2));
//
//                                            final Outlet<Integer> source = builder.add(in).out();
//                                            builder
//                                                    .from(source)
//                                                    .via(builder.add(f1))
//                                                    .viaFanOut(bcast)
//                                                    .via(builder.add(f2))
//                                                    .viaFanIn(merge)
//                                                    .via(builder.add(f3.grouped(1000)))
//                                                    .to(out); // to() expects a SinkShape
//                                            builder.from(bcast).via(builder.add(f4)).toFanIn(merge);
//                                            return ClosedShape.getInstance();
//                                        }));





        System.out.println("Hello World from financial DSL");
    }

    List<Transaction> getTransactionFromAuth(Auth auth, List<Tender> tenders, List<Settlement> settlements, ActorSystem system){


        RunnableGraph.fromGraph(
                GraphDSL.create(
                        Sink.ignore(),
                        (builder,out) -> {
                            final Outlet<String> source = builder.add(Source.single(auth.paymentId())).out();
                            final UniformFanOutShape<String, String> paymentIdBCast = builder.add(Broadcast.create(2));
                            final Flow<String, List<Tender>, NotUsed> tenderInitial =
                                    tenderRetrieveFlow(tenders, (Tender tender, String key) -> {return tender.paymentId() == key;}, system);
                            final Flow<String, List<Settlement>, NotUsed> settlementInitial =
                                    settlementRetrieveFlow(settlements, (Settlement settlement, String key) -> {return settlement.paymentId() == key;}, system);


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


    Flow<String, List<Tender>, NotUsed> tenderRetrieveFlow(List<Tender> tenders, BiFunction<Tender, String, Boolean> bi, ActorSystem system){
        return Flow.of(String.class).map(key ->
                {
                    CompletionStage<List<Tender>> getMatchingTenders =  Source
                            .from(tenders)
                            .filter(tender -> bi.apply(tender, key))
                            .runWith(Sink.collect(Collectors.toList()), system);
                    return getMatchingTenders.toCompletableFuture().get();
                });
    }

    Flow<String, List<Settlement>, NotUsed> settlementRetrieveFlow(List<Settlement> settlements, BiFunction<Settlement, String, Boolean> bi, ActorSystem system){
        return Flow.of(String.class).map(key ->
        {
            CompletionStage<List<Settlement>> getMatchingSettlement =  Source
                    .from(settlements)
                    .filter(settlement -> bi.apply(settlement, key))
                    .runWith(Sink.collect(Collectors.toList()), system);
            return getMatchingSettlement.toCompletableFuture().get();
        });
    }

}


