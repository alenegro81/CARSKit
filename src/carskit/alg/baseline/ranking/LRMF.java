// Copyright (C) 2015 Yong Zheng
//
// This file is part of CARSKit.
//
// CARSKit is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// CARSKit is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with CARSKit. If not, see <http://www.gnu.org/licenses/>.
//

package carskit.alg.baseline.ranking;

import happy.coding.io.Strings;

import java.util.List;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;

import carskit.data.structure.SparseMatrix;
import carskit.generic.IterativeRecommender;
import carskit.data.setting.Configuration;

/**
 * LRMF: Shi, Yue, Martha Larson, and Alan Hanjalic. "List-wise learning to rank with matrix factorization for collaborative filtering." Proceedings of the fourth ACM conference on Recommender systems. ACM, 2010.
 * <p></p>
 * Note: This implementation is modified from the algorithm in LibRec-v1.3
 *
 * @author Yong Zheng
 *
 */

@Configuration("binThold, numFactors, initLRate, maxLRate, regU, regI, numIters")
public class LRMF extends IterativeRecommender {
    public DenseVector userExp;

    public LRMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
        super(trainMatrix, testMatrix, fold);

        isRankingPred = true;
        initByNorm = false;

        this.algoName = "LRMF";
    }

    @Override
    protected void initModel() throws Exception {
        super.initModel();

        userExp = new DenseVector(numUsers);
        for (MatrixEntry me : train) {
            int u = me.row(); // user
            double ruj = me.get();

            userExp.add(u, Math.exp(ruj));
        }

    }

    @Override
    protected void buildModel() throws Exception {

        for (int iter = 1; iter <= numIters; iter++) {

            loss = 0;
            for (MatrixEntry me : train) {

                int u = me.row(); // user
                int j = me.column(); // item
                double ruj = me.get();

                double pred = DenseMatrix.rowMult(P, u, Q, j);
                double uexp = 0;

                List<Integer> items = train.getColumns(u);
                for (int i : items) {
                    uexp += Math.exp(DenseMatrix.rowMult(P, u, Q, i));
                }

                loss -= Math.exp(ruj) / userExp.get(u) * Math.log(Math.exp(pred) / uexp);

                // update factors
                for (int f = 0; f < numFactors; f++) {
                    double puf = P.get(u, f);
                    double qjf = Q.get(j, f);
                    double delta_u = (Math.exp(ruj) / userExp.get(u) - Math.exp(pred) / uexp) * gd(pred) * qjf - regU * puf;
                    double delta_j = (Math.exp(ruj) / userExp.get(u) - Math.exp(pred) / uexp) * gd(pred) * puf - regI * qjf;

                    P.add(u, f, lRate * delta_u);
                    Q.add(j, f, lRate * delta_j);

                    loss += 0.5 * regU * puf * puf + 0.5 * regI * qjf * qjf;
                }

            }

            if (isConverged(iter))
                break;

        }// end of training
    }

    @Override
    public String toString() {
        return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, numIters }, ",");
    }
}